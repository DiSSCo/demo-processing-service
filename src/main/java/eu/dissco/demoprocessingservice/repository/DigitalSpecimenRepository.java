package eu.dissco.demoprocessingservice.repository;

import static eu.dissco.demoprocessingservice.database.jooq.Tables.DIGITAL_SPECIMEN;
import static eu.dissco.demoprocessingservice.database.jooq.Tables.MEDIA_OBJECT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.database.jooq.enums.Curatedobjectidtypes;
import eu.dissco.demoprocessingservice.domain.Image;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DigitalSpecimenRepository {

  private final ObjectMapper mapper;
  private final DSLContext context;

  public List<OpenDSWrapper> digitalSpecimen(List<String> batch) {
    return context.select(DIGITAL_SPECIMEN.ID, DIGITAL_SPECIMEN.DATA)
        .from(DIGITAL_SPECIMEN)
        .where(DIGITAL_SPECIMEN.CURATED_OBJECT_ID.in(batch))
        .fetch(this::mapToDigitalSpecimen).stream().filter(Objects::nonNull).toList();
  }

  private OpenDSWrapper mapToDigitalSpecimen(Record2<String, JSONB> dataRecord) {
    var dataString = dataRecord.get(DIGITAL_SPECIMEN.DATA).data();
    try {
      var ds = mapper.readValue(dataString, OpenDSWrapper.class);
      ds.setId(dataRecord.get(DIGITAL_SPECIMEN.ID));
      return ds;
    } catch (JsonProcessingException e) {
      log.error("Failed to parse record: {}, skipping record", dataRecord, e);
      return null;
    }
  }

  public void commitUpsertObject(Collection<OpenDSWrapper> values) throws JsonProcessingException {
    var queryList = new ArrayList<Query>();
    for (OpenDSWrapper value : values) {
      var query = context.insertInto(DIGITAL_SPECIMEN)
          .values(value.getId(),
              value.getType(),
              value.getAuthoritative().getPhysicalSpecimenId(),
              value.getAuthoritative().getMidslevel(),
              Curatedobjectidtypes.physicalSpecimenID,
              value.getAuthoritative().getName(),
              value.getAuthoritative().getInstitutionCode(),
              value.getAuthoritative().getInstitution(),
              mapper.valueToTree(value))
          .onConflict(DIGITAL_SPECIMEN.ID)
          .doUpdate()
          .set(DIGITAL_SPECIMEN.OBJECT_TYPE, value.getType())
          .set(DIGITAL_SPECIMEN.MIDS_LEVEL, (short) value.getAuthoritative().getMidslevel())
          .set(DIGITAL_SPECIMEN.CURATED_OBJECT_ID_TYPE, Curatedobjectidtypes.physicalSpecimenID)
          .set(DIGITAL_SPECIMEN.SPECIMEN_NAME, value.getAuthoritative().getName())
          .set(DIGITAL_SPECIMEN.INSTITUTION_NAME, value.getAuthoritative().getInstitutionCode())
          .set(DIGITAL_SPECIMEN.INSTITUTION_ID, value.getAuthoritative().getInstitution())
          .set(DIGITAL_SPECIMEN.DATA, JSONB.jsonb(mapper.writeValueAsString(value)));
      queryList.add(query);
    }
    context.batch(queryList).execute();
  }

  public void commitImages(List<OpenDSWrapper> updatedItems) {
    var queryList = new ArrayList<Query>();
    for (OpenDSWrapper updatedItem : updatedItems) {
      if (updatedItem != null && !updatedItem.getImages().isEmpty()) {
        for (Image image : updatedItem.getImages()) {
          var query = context.insertInto(MEDIA_OBJECT)
              .values(
                  UUID.randomUUID(),
                  "Media-Object",
                  updatedItem.getId(),
                  image.getImageUri(),
                  mapper.valueToTree(image)
              );
          queryList.add(query);
        }
      }
    }
    context.batch(queryList).execute();
  }
}
