package org.gentar.biology.plan.engine.es_cell.processors;

import org.gentar.biology.plan.Plan;
import org.gentar.biology.plan.PlanQueryHelper;
import org.gentar.biology.plan.engine.PlanStateSetter;
import org.gentar.statemachine.AbstractProcessor;
import org.gentar.statemachine.ProcessData;
import org.gentar.statemachine.ProcessEvent;
import org.gentar.statemachine.TransitionEvaluation;
import org.springframework.stereotype.Component;

@Component
public class AbortChimerasFounderObtainedProcessor extends AbstractProcessor
{
    public AbortChimerasFounderObtainedProcessor(PlanStateSetter planStateSetter)
    {
        super(planStateSetter);
    }

    @Override
    public TransitionEvaluation evaluateTransition(ProcessEvent transition, ProcessData data)
    {
        TransitionEvaluation transitionEvaluation = new TransitionEvaluation();
        transitionEvaluation.setTransition(transition);
        boolean areAllColoniesAborted = areAllColoniesAborted((Plan) data);
        transitionEvaluation.setExecutable(areAllColoniesAborted);
        if (!areAllColoniesAborted)
        {
            transitionEvaluation.setNote(
                    "The plan has colonies that are not aborted. Please abort them first");
        }
        return transitionEvaluation;
    }

    private boolean areAllColoniesAborted(Plan plan)
    {
        return PlanQueryHelper.areAllColoniesByPlanAborted(plan);
    }
}
