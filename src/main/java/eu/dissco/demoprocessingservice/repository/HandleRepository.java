package eu.dissco.demoprocessingservice.repository;

import static eu.dissco.demoprocessingservice.database.jooq.Tables.HANDLES;

import eu.dissco.demoprocessingservice.database.jooq.tables.records.HandlesRecord;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Query;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HandleRepository {

  private static final String PATTERN_FORMAT = "yyyy-MM-dd";

  private final DSLContext context;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
      .withZone(ZoneOffset.UTC);

  public List<String> checkHandles(List<String> ids) {
    var idsByte = ids.stream().map(id -> id.getBytes(StandardCharsets.UTF_8)).toList();
    return context.selectDistinct(HANDLES.HANDLE).from(HANDLES).where(HANDLES.HANDLE.in(idsByte))
        .fetch(this::mapHandle);
  }

  private String mapHandle(Record1<byte[]> record1) {
    return new String(record1.value1(), StandardCharsets.UTF_8);
  }

  public void commitNewHandles(List<OpenDSWrapper> newItems) {
    var queryList = new ArrayList<Query>();
    for (var newItem : newItems) {
      queryList.add(addProperty(newItem.getId(), 1, "URL",
          "https://sandbox.dissco.tech/api/v1/specimen/" + newItem.getId()));
      queryList.add(addProperty(newItem.getId(), 2, "pidIssuer",
          "{\"pid\":\"20.5000.1025/\", \"nameFromPid\":\"DiSSCo\"}"));
      queryList.add(addProperty(newItem.getId(), 3, "digitalObjectType", ""));
      queryList.add(addProperty(newItem.getId(), 4, "issueDate", formatter.format(Instant.now())));
      queryList.add(addProperty(newItem.getId(), 5, "IssueNumber", "1"));
      queryList.add(addProperty(newItem.getId(), 6, "pidStatus", "ACTIVE"));
      queryList.add(
          addProperty(newItem.getId(), 7, "pidKernelMetadataLicense", "Creative Commons Zero"));
      queryList.add(addProperty(newItem.getId(), 8, "referent", "to be added"));
      queryList.add(addProperty(newItem.getId(), 100, "HS_ADMIN", "300:0.NA/20.5000.1025"));
    }
    context.batch(queryList).execute();
  }

  private InsertSetMoreStep<HandlesRecord> addProperty(String id, int index, String type,
      String data) {
    return context.insertInto(HANDLES)
        .set(HANDLES.HANDLE, id.getBytes(StandardCharsets.UTF_8))
        .set(HANDLES.IDX, index)
        .set(HANDLES.TYPE, type.getBytes(StandardCharsets.UTF_8))
        .set(HANDLES.DATA, data.getBytes(
            StandardCharsets.UTF_8))
        .set(HANDLES.TTL_TYPE, (short) 0)
        .set(HANDLES.TTL, 864000)
        .set(HANDLES.TIMESTAMP, Instant.now().getEpochSecond())
        .set(HANDLES.ADMIN_READ, Boolean.TRUE)
        .set(HANDLES.ADMIN_WRITE, Boolean.TRUE)
        .set(HANDLES.PUB_READ, Boolean.TRUE)
        .set(HANDLES.PUB_WRITE, Boolean.FALSE);
  }
}
