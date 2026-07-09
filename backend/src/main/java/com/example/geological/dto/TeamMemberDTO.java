package com.example.geological.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamMemberDTO {

    private Long id;

    @NotBlank(message = "队员编号不能为空")
    private String memberNo;

    @NotBlank(message = "队员姓名不能为空")
    private String memberName;

    private String phone;

    private String position;

    @NotNull(message = "所属小队不能为空")
    private Long teamId;

    private String teamName;
}