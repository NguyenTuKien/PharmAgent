package ct01.n07.backend.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSessionsResponse {
    private int totalSessions;
    private List<SessionInfo> sessions;
}
