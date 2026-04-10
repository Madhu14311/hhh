package com.blisssierra.hrms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LeaveResponseDTO {

    private String status;
    private String message;

    /**
     * employees.id — the numeric PK the frontend needs for all subsequent API calls
     */
    private Long userId;

    private String name;
    private String email;
    private String empId;
    private String designation;
    private String faceImagePaths;

}
