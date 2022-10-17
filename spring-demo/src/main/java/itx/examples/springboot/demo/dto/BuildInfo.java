package itx.examples.springboot.demo.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class BuildInfo {

    private final String time;
    private final String version;
    private final String name;
    private final String artifact;
    private final String group;
    private final String gitFullHash;
    private final String gitBranchName;

}
