package org.gentar.biology.plan.attempt.phenotyping.stage.engine.processors;

import org.gentar.biology.plan.attempt.phenotyping.stage.PhenotypingStage;
import org.gentar.biology.plan.attempt.phenotyping.stage.PhenotypingStageStateSetter;
import org.gentar.biology.plan.attempt.phenotyping.stage.engine.PhenotypingStageEvent;
import org.gentar.biology.plan.attempt.phenotyping.stage.engine.PhenotypingStageState;
import org.gentar.biology.status.Status;
import org.gentar.exceptions.UserOperationFailedException;
import org.gentar.security.abac.spring.ContextAwarePolicyEnforcement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllDataProcessedToPhenotypingFinishedProcessorTest
{

    private AllDataProcessedToPhenotypingFinishedProcessor testInstance;

    @Mock
    private PhenotypingStageStateSetter stageStateSetter;

    @Mock
    private ContextAwarePolicyEnforcement policyEnforcement;

    @BeforeEach
    void setUp()
    {
        testInstance = new AllDataProcessedToPhenotypingFinishedProcessor(
            stageStateSetter, policyEnforcement);
    }

    @Test
    public void testWhenUserIsAllowed()
    {
        PhenotypingStage phenotypingStage = buildPhenotypingStage(
            PhenotypingStageState.PhenotypingAllDataProcessed.getInternalName());
        phenotypingStage.setEvent(PhenotypingStageEvent.updateToPhenotypingFinished);
        when(policyEnforcement.hasPermission(any(), anyString())).thenReturn(true);

        testInstance.process(phenotypingStage);

        verify(
            stageStateSetter, times(1)).setStatusByName(
            any(PhenotypingStage.class),
            eq(PhenotypingStageState.PhenotypingFinished.getInternalName()));
    }

    @Test
    public void testWhenUserIsNotAllowed()
    {
        PhenotypingStage phenotypingStage = buildPhenotypingStage(
            PhenotypingStageState.PhenotypingAllDataProcessed.getInternalName());
        phenotypingStage.setEvent(PhenotypingStageEvent.updateToPhenotypingFinished);
        when(policyEnforcement.hasPermission(any(), anyString())).thenReturn(false);

        UserOperationFailedException thrown = assertThrows(UserOperationFailedException.class,
            () -> testInstance.process(phenotypingStage), "Exception not thrown");
        assertTransitionCannotBeExecuted(thrown);
    }

    private void assertTransitionCannotBeExecuted(UserOperationFailedException thrown)
    {
        assertThat(
            "Not expected message", thrown.getMessage(), is("Transition cannot be executed The current user does not have permission to move to 'Phenotyping Finished'"));
              verify(
            stageStateSetter, times(0)).setStatusByName(
            any(PhenotypingStage.class), any(String.class));
    }

    private PhenotypingStage buildPhenotypingStage(String statusName)
    {
        PhenotypingStage phenotypingStage = new PhenotypingStage();
        Status status = new Status();
        status.setName(statusName);
        phenotypingStage.setStatus(status);
        return phenotypingStage;
    }
}