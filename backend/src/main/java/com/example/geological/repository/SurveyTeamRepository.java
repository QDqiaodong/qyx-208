package com.example.geological.repository;

import com.example.geological.entity.SurveyTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyTeamRepository extends JpaRepository<SurveyTeam, Long> {

    Optional<SurveyTeam> findByTeamCode(String teamCode);

    Optional<SurveyTeam> findByTeamName(String teamName);

    List<SurveyTeam> findByStatus(Integer status);
}