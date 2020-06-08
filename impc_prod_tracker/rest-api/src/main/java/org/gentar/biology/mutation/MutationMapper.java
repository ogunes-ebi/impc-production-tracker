package org.gentar.biology.mutation;

import org.gentar.EntityMapper;
import org.gentar.Mapper;
import org.gentar.biology.gene.GeneMapper;
import org.springframework.stereotype.Component;
import java.util.HashSet;

@Component
public class MutationMapper implements Mapper<Mutation, MutationDTO>
{
    private EntityMapper entityMapper;
    private MutationQCResultMapper mutationQCResultMapper;
    private MutationCategorizationMapper mutationCategorizationMapper;
    private GeneMapper geneMapper;
    private MutationSequenceMapper mutationSequenceMapper;

    public MutationMapper(
        EntityMapper entityMapper,
        MutationQCResultMapper mutationQCResultMapper,
        MutationCategorizationMapper mutationCategorizationMapper,
        GeneMapper geneMapper,
        MutationSequenceMapper mutationSequenceMapper)
    {
        this.entityMapper = entityMapper;
        this.mutationQCResultMapper = mutationQCResultMapper;
        this.mutationCategorizationMapper = mutationCategorizationMapper;
        this.geneMapper = geneMapper;
        this.mutationSequenceMapper = mutationSequenceMapper;
    }

    @Override
    public MutationDTO toDto(Mutation mutation)
    {
        MutationDTO mutationDTO = entityMapper.toTarget(mutation, MutationDTO.class);
        mutationDTO.setMutationQCResultDTOs(
            mutationQCResultMapper.toDtos(mutation.getMutationQcResults()));
        mutationDTO.setGeneDTOS(geneMapper.toDtos(mutation.getGenes()));
        mutationDTO.setMutationSequenceDTOS(
            mutationSequenceMapper.toDtos(mutation.getMutationSequences()));
        mutationDTO.setMutationCategorizationDTOS(
            mutationCategorizationMapper.toDtos(mutation.getMutationCategorizations()));
        return mutationDTO;
    }

    @Override
    public Mutation toEntity(MutationDTO mutationDTO)
    {
        Mutation mutation = entityMapper.toTarget(mutationDTO, Mutation.class);
        mutation.setMutationQcResults(
            new HashSet<>(mutationQCResultMapper.toEntities(mutationDTO.getMutationQCResultDTOs())));
        mutation.setGenes(new HashSet<>(geneMapper.toEntities(mutationDTO.getGeneDTOS())));
        mutation.setMutationSequences(
            new HashSet<>(mutationSequenceMapper.toEntities(mutationDTO.getMutationSequenceDTOS())));
        mutation.setMutationCategorizations(
            new HashSet<>(mutationCategorizationMapper.toEntities(
                mutationDTO.getMutationCategorizationDTOS())));
        return mutation;
    }
}
