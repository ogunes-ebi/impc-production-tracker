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
package uk.ac.ebi.impc_prod_tracker.service.plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.impc_prod_tracker.common.Constants;
import uk.ac.ebi.impc_prod_tracker.conf.security.abac.ResourceAccessChecker;
import uk.ac.ebi.impc_prod_tracker.data.biology.outcome.Outcome;
import uk.ac.ebi.impc_prod_tracker.data.biology.outcome.OutcomeRepository;
import uk.ac.ebi.impc_prod_tracker.data.experiment.colony.Colony;
import uk.ac.ebi.impc_prod_tracker.data.experiment.plan.Plan;
import uk.ac.ebi.impc_prod_tracker.data.experiment.plan.PlanRepository;
import uk.ac.ebi.impc_prod_tracker.data.experiment.project.Project;
import java.util.List;

@Component
public class PlanServiceImpl implements PlanService
{
    private PlanRepository planRepository;
    private OutcomeRepository outcomeRepository;
    private ResourceAccessChecker<Plan> resourceAccessChecker;

    private static final String READ_PLAN_ACTION = "READ_PLAN";

    PlanServiceImpl(
        PlanRepository planRepository,
        OutcomeRepository outcome,
        ResourceAccessChecker<Plan> resourceAccessChecker)
    {
        this.planRepository = planRepository;
        this.outcomeRepository = outcome;
        this.resourceAccessChecker = resourceAccessChecker;
    }

    @Override
    public Plan getProductionPlanRefByPhenotypePlan(Plan phenotypePlan)
    {
        Plan plan = null;
        if (Constants.PHENOTYPE_TYPE.equals(phenotypePlan.getPlanType().getName()))
        {
            Colony parentColony = phenotypePlan.getColony();
            List<Outcome> outcomesByColony = outcomeRepository.findAllByColony(parentColony);
            for (Outcome outcome : outcomesByColony)
            {
                plan = planRepository.findPlanById(outcome.getAttempt().getId());
                break;
            }
        }

        return plan;
    }

    @Override
    public List<Plan> getPlansByProject(Project project)
    {
        List<Plan> plans = planRepository.findAllByProject(project);
        return getAccessCheckedPlans(plans);
    }

    @Override
    public Plan getPlanByPin(String pin)
    {
        Plan plan = planRepository.findPlanByPin(pin);
        return getAccessCheckedPlan(plan);
    }

    @Override
    public List<Plan> getPlans()
    {
        List<Plan> plans = planRepository.findAll();
        return getAccessCheckedPlans(plans);
    }

    @Override
    public Page<Plan> getPaginatedPlans(Pageable pageable)
    {
        Page<Plan> plans = planRepository.findAll(pageable);
        return plans.map(this::getAccessCheckedPlan);
    }

    private Plan getAccessCheckedPlan(Plan plan)
    {
        return (Plan) resourceAccessChecker.checkAccess(plan, READ_PLAN_ACTION);
    }

    private List<Plan> getAccessCheckedPlans(List<Plan> plans)
    {
       return (List<Plan>) resourceAccessChecker.checkAccessForCollection(plans, READ_PLAN_ACTION);
    }
}
