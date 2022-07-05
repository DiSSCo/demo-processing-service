package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.demoprocessingservice.domain.Image;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateService {

  public OpenDSWrapper updateObject(OpenDSWrapper newObject, String eventType,
      OpenDSWrapper existingObject) {
    if (newObject.equals(existingObject)){
      return null;
    }
    log.debug("Objects are not equal, update existing object");
    var finalObject = new OpenDSWrapper();
    if (eventType.equals("eu.dissco.translator.event")) {
      updateAuthoratitySection(newObject, existingObject, finalObject);
    } else {
      finalObject.setAuthoritative(existingObject.getAuthoritative());
      finalObject.setUnmapped(existingObject.getUnmapped());
    }
    finalObject.setImages(getUpdatedImages(newObject, existingObject));
    finalObject.setId(existingObject.getId());
    finalObject.setType(existingObject.getType());

    return finalObject;
  }

  private void updateAuthoratitySection(OpenDSWrapper newObject, OpenDSWrapper existingObject,
      OpenDSWrapper finalObject) {
    if (!existingObject.getAuthoritative().equals(newObject.getAuthoritative())) {
      // Assume the latest version is the most valid/complete
      finalObject.setAuthoritative(newObject.getAuthoritative());
    } else {
      finalObject.setAuthoritative(existingObject.getAuthoritative());
    }
    if (existingObject.getUnmapped() == null && newObject.getUnmapped() == null) {
      finalObject.setUnmapped(null);
    } else if (existingObject.getUnmapped() == null && newObject.getUnmapped() != null) {
      finalObject.setUnmapped(newObject.getUnmapped());
    } else if (existingObject.getUnmapped() != null && newObject.getUnmapped() == null) {
      finalObject.setUnmapped(existingObject.getUnmapped());
    } else {
      if (!existingObject.getUnmapped().equals(newObject.getUnmapped())) {
        finalObject.setUnmapped(newObject.getUnmapped());
      } else {
        finalObject.setUnmapped(existingObject.getUnmapped());
      }
    }
  }

  private List<Image> getUpdatedImages(OpenDSWrapper newObject, OpenDSWrapper existingObject) {
    if (existingObject.getImages() == null && newObject.getImages() != null) {
      return newObject.getImages();
    } else if ((existingObject.getImages() != null && newObject.getImages() == null) || (
        existingObject.getImages() == null && newObject.getImages() == null)) {
      return existingObject.getImages();
    } else if (existingObject.getImages().equals(newObject.getImages())) {
      // Images are equal no changes have been made
      return existingObject.getImages();
    }
    return updateImages(newObject, existingObject);
  }

  private ArrayList<Image> updateImages(OpenDSWrapper newObject, OpenDSWrapper existingObject) {
    var finalImage = new ArrayList<Image>();
    var existingImageMap = existingObject.getImages().stream()
        .collect(HashMap<String, List<JsonNode>>::new,
            (m, v) -> m.put(v.getImageUri(), v.getAdditionalInfo()), HashMap::putAll);
    var newImageMap = newObject.getImages().stream()
        .collect(HashMap<String, List<JsonNode>>::new,
            (m, v) -> m.put(v.getImageUri(), v.getAdditionalInfo()), HashMap::putAll);
    for (var newImage : newImageMap.entrySet()) {
      var existingImage = existingImageMap.get(newImage.getKey());
      if (existingImage == null) {
        // New Image added
        finalImage.add(createImage(newImage.getKey(), newImage.getValue()));
      } else if (newImage.getValue() == null) {
        finalImage.add(createImage(newImage.getKey(), existingImage));
      } else if (!newImage.getValue().equals(existingImage)) {
        // If additional info new image differs to existing
        finalImage.add(createImage(newImage.getKey(),
            updateAdditionalInfo(newImage.getValue(), existingImage)));
      } else {
        // Image is in both and equal
        finalImage.add(createImage(newImage.getKey(), newImage.getValue()));
      }
    }
    for (var existingImage : existingImageMap.entrySet()) {
      if (!newImageMap.containsKey(existingImage.getKey())) {
        // New object is missing image from existing object
        finalImage.add(createImage(existingImage.getKey(), existingImage.getValue()));
      }
    }
    return finalImage;
  }

  private Image createImage(String url, List<JsonNode> additionalInfo) {
    var image = new Image();
    image.setImageUri(url);
    image.setAdditionalInfo(additionalInfo);
    return image;
  }

  private List<JsonNode> updateAdditionalInfo(List<JsonNode> newImage,
      List<JsonNode> existingImage) {
    var newAdditionalInfoMap = newImage.stream()
        .collect(Collectors.toMap(e -> e.get("source").asText(), Function.identity()));
    var existingAdditionalInfoMap = existingImage.stream()
        .collect(Collectors.toMap(e -> e.get("source").asText(), Function.identity()));
    var finalList = new ArrayList<JsonNode>();
    for (var newAdditionalInfo : newAdditionalInfoMap.entrySet()) {
      if (existingAdditionalInfoMap.containsKey(newAdditionalInfo.getKey())) {
        // Assume last update is of the highest quality
        finalList.add(newAdditionalInfo.getValue());
      } else {
        // Additional info not yet present
        finalList.add(newAdditionalInfo.getValue());
      }
    }
    for (var existingAdditionalInfo : existingAdditionalInfoMap.entrySet()) {
      if (!newAdditionalInfoMap.containsKey(existingAdditionalInfo.getKey())) {
        // Older information not present in new object
        finalList.add(existingAdditionalInfo.getValue());
      }
    }
    return finalList;
  }

}
