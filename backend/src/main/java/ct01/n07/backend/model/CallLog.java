package ct01.n07.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "call_logs")
public class CallLog {
    @Id
    private String id;

    @Indexed
    private String callerId;

    @Indexed
    private String receiverId;

    @CreatedDate
    private Instant startedAt;

    private Instant endedAt;

    private Long durationInSeconds;

    // e.g., COMPLETED, REJECTED, MISSED
    private String status;
}
