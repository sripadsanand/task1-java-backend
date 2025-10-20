package com.kaiburr.demo.model;

import lombok.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class TaskExecution {
    private Date startTime;
    private Date endTime;
    private String output;
}
