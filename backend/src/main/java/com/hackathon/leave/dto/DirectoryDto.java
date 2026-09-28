package com.hackathon.leave.dto;

import java.util.List;

/** Lookup data for pickers: the teams (with headcount) and the managers a delegation can go to. */
public record DirectoryDto(List<TeamInfo> teams, List<ManagerInfo> managers) {
    public record TeamInfo(Long id, String name, int size) {}

    public record ManagerInfo(Long id, String name, String teamName) {}
}
