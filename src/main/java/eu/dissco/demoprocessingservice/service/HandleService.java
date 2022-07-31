package eu.dissco.demoprocessingservice.service;

import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.domain.UpdatedDS;
import eu.dissco.demoprocessingservice.domain.UpdatedDSStatus;
import eu.dissco.demoprocessingservice.repository.HandleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleService {

  private final HandleRepository repository;

  private List<String> getIds(List<OpenDSWrapper> newItems) {
    return newItems.stream().map(OpenDSWrapper::getId).toList();
  }

  public void commitHandles(List<UpdatedDS> updatedItems) {
    List<OpenDSWrapper> newItems = updatedItems.stream()
        .filter(updatedDS -> updatedDS.status().equals(
            UpdatedDSStatus.NEW)).map(UpdatedDS::openDS).toList();
    ensureUniqueHandles(newItems);
    repository.commitNewHandles(newItems);
  }


  private void ensureUniqueHandles(List<OpenDSWrapper> newItems) {
    List<String> nonUniqueHandles = repository.checkHandles(getIds(newItems));
    if (!nonUniqueHandles.isEmpty()) {
      List<OpenDSWrapper> nonUniqueRecords = newItems.stream()
          .filter(openDSWrapper -> nonUniqueHandles.contains(openDSWrapper.getId())).toList();
      for (var nonUniqueRecord : nonUniqueRecords) {
        nonUniqueRecord.setId("20.5000.1025/" + UUID.randomUUID());
      }
      ensureUniqueHandles(nonUniqueRecords);
    }
  }
}
