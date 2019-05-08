/*******************************************************************************
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 *******************************************************************************/
package uk.ac.ebi.impc_prod_tracker.controller.project;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.impc_prod_tracker.conf.exeption_management.OperationFailedException;
import uk.ac.ebi.impc_prod_tracker.controller.project.plan.PlanDTO;
import uk.ac.ebi.impc_prod_tracker.controller.project.plan.PlanDTOBuilder;
import uk.ac.ebi.impc_prod_tracker.data.experiment.plan.Plan;
import uk.ac.ebi.impc_prod_tracker.data.experiment.project.Project;
import uk.ac.ebi.impc_prod_tracker.service.plan.PlanService;
import uk.ac.ebi.impc_prod_tracker.service.project.ProjectService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200"})
public class ProjectController
{
    private ProjectService projectService;
    private PlanService planService;
    private PlanDTOBuilder planDTOBuilder;
    private ProjectDTOBuilder projectDTOBuilder;

    ProjectController(
        ProjectService projectService,
        ProjectDTOBuilder projectDTOBuilder,
        PlanDTOBuilder planDTOBuilder,
        PlanService planService)
    {
        this.projectService = projectService;
        this.projectDTOBuilder = projectDTOBuilder;
        this.planDTOBuilder = planDTOBuilder;
        this.planService = planService;
    }

    @GetMapping(value = {"/projects"})
    public Map<String, List<ProjectDTO>> getPlansMap()
    {
        List<Project> projects = projectService.getProjects();

        List<ProjectDTO> projectDTOList = projects.stream()
            .map(project -> projectDTOBuilder.buildProjectDTOFromProject(project))
            .collect(Collectors.toList());
        Map<String, List<ProjectDTO>> map = new HashMap<>();
        map.put("projects", projectDTOList);

        return map;
    }

    @GetMapping(value = {"/projects/{tpn}"})
    public ProjectDTO getProjects(@PathVariable String tpn)
    {
        Project project = projectService.getProjectByTpn(tpn);
        if (project == null)
        {
            throw new OperationFailedException(
                String.format("The project %s does not exist", tpn));
        }
        return projectDTOBuilder.buildProjectDTOFromProject(project);
    }

    @GetMapping(value = {"/projects/{tpn}/plans/{pin}"})
    public ProjectPlanDTO getProjectPlan(@PathVariable String tpn, @PathVariable String pin)
    {
        Project project = projectService.getProjectByTpn(tpn);
        if (project == null)
        {
            throw new OperationFailedException(
                String.format("The project %s does not exist", tpn));
        }
        List<Plan> plans = planService.getPlansByProject(project);
        PlanDTO planDTO = null;
        for (Plan plan : plans)
        {
            if (plan.getPin().equals(pin))
            {
                planDTO = planDTOBuilder.buildPlanDTOFromPlan(plan);
                break;
            }
        }
        if (planDTO == null)
        {
            throw new OperationFailedException(
                String.format("Project %s does not have any plan %s associated", tpn, pin),
                HttpStatus.NOT_FOUND);
        }
        Map<String, List<Object>> map = new HashMap<>();
        map.put("res", Arrays.asList(project, planDTO));

        ProjectPlanDTO projectPlanDTO = new ProjectPlanDTO();
        projectPlanDTO.setProjectDetailsDTO(projectDTOBuilder.buildProjectDetailsDTOFromProject(project));
        projectPlanDTO.setPlanDTO(planDTO);
        return projectPlanDTO;
    }
}
