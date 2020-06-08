package org.gentar.biology.mutation;

import org.gentar.EntityMapper;
import org.gentar.biology.gene.Gene;
import org.gentar.biology.gene.GeneMapper;
import org.gentar.biology.mutation.categorizarion.MutationCategorization;
import org.gentar.biology.mutation.genbank_file.GenbankFile;
import org.gentar.biology.mutation.genetic_type.GeneticMutationType;
import org.gentar.biology.mutation.molecular_type.MolecularMutationType;
import org.gentar.biology.outcome.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class MutationCommonMapperTest
{
    public static final long ID = 1L;
    public static final String MGI_ALLELE_ID = "mgiAlleleId";
    public static final String MGI_ALLELE_SYMBOL = "mgiAlleleSymbol";
    public static final boolean MGI_ALLELE_SYMBOL_REQUIRES_CONSTRUCTION = true;
    public static final boolean MGI_ALLELE_SYMBOL_WITHOUT_IMPC_ABBREVIATION = true;
    public static final boolean ALLELE_CONFIRMED = false;
    public static final String ALLELE_SYMBOL_SUPERSCRIPT_TEMPLATE = "alleleSymbolSuperscriptTemplate";
    public static final String DESCRIPTION = "description";
    public static final String AUTO_DESCRIPTION = "autoDescription";
    public static final long IMITS_ALLELE = 2L;
    public static final String GENETIC_MUTATION_TYPE = "geneticMutationType";
    public static final String MOLECULAR_MUTATION_TYPE = "molecularMutationType";
    public static final String FILE_GB = "fileGb";
    public static final String SYMBOL = "symbol";
    public static final String TPO = "tpo";
    public static final String MUTATION_CATEGORIZATION = "mutationCategorization";

    private MutationCommonMapper testInstance;

    @Autowired
    private EntityMapper entityMapper;

    @Mock
    private MutationQCResultMapper mutationQCResultMapper;
    @Mock
    private MutationCategorizationMapper mutationCategorizationMapper;
    @Mock
    private GeneMapper geneMapper;
    @Mock
    private MutationSequenceMapper mutationSequenceMapper;

    @BeforeEach
    void setUp()
    {
        testInstance =
            new MutationCommonMapper(
                entityMapper,
                mutationQCResultMapper,
                mutationCategorizationMapper,
                geneMapper,
                mutationSequenceMapper);
    }

    @Test
    void toDto()
    {
        Mutation mutation = buildMutation();
        MutationCommonDTO mutationCommonDTO = testInstance.toDto(mutation);

        assertThat(
            mutationCommonDTO.getMgiAlleleSymbolRequiresConstruction(),
            is(MGI_ALLELE_SYMBOL_REQUIRES_CONSTRUCTION));
        assertThat(mutationCommonDTO.getAlleleConfirmed(), is(ALLELE_CONFIRMED));
        assertThat(mutationCommonDTO.getImitsAllele(), is(IMITS_ALLELE));
        assertThat(mutationCommonDTO.getGeneticMutationTypeName(), is(GENETIC_MUTATION_TYPE));
        assertThat(mutationCommonDTO.getMolecularMutationTypeName(), is(MOLECULAR_MUTATION_TYPE));
        assertThat(mutationCommonDTO.getGenbankFileName(), is(FILE_GB));

        verify(mutationQCResultMapper, times(1)).toDtos(mutation.getMutationQcResults());
        verify(geneMapper, times(1)).toDtos(mutation.getGenes());
        verify(mutationSequenceMapper, times(1)).toDtos(mutation.getMutationSequences());
        verify(mutationCategorizationMapper, times(1)).toDtos(mutation.getMutationCategorizations());
    }

    private Mutation buildMutation()
    {
        Mutation mutation = new Mutation();
        mutation.setId(ID);
        mutation.setMgiAlleleId(MGI_ALLELE_ID);
        mutation.setMgiAlleleSymbol(MGI_ALLELE_SYMBOL);
        mutation.setMgiAlleleSymbolRequiresConstruction(MGI_ALLELE_SYMBOL_REQUIRES_CONSTRUCTION);
        mutation.setMgiAlleleSymbolWithoutImpcAbbreviation(MGI_ALLELE_SYMBOL_WITHOUT_IMPC_ABBREVIATION);
        mutation.setAlleleConfirmed(ALLELE_CONFIRMED);
        mutation.setAlleleSymbolSuperscriptTemplate(ALLELE_SYMBOL_SUPERSCRIPT_TEMPLATE);
        mutation.setDescription(DESCRIPTION);
        mutation.setAutoDescription(AUTO_DESCRIPTION);
        mutation.setImitsAllele(IMITS_ALLELE);
        GeneticMutationType geneticMutationType = new GeneticMutationType();
        geneticMutationType.setName(GENETIC_MUTATION_TYPE);
        mutation.setGeneticMutationType(geneticMutationType);
        MolecularMutationType molecularMutationType = new MolecularMutationType();
        molecularMutationType.setName(MOLECULAR_MUTATION_TYPE);
        mutation.setMolecularMutationType(molecularMutationType);
        GenbankFile genbankFile = new GenbankFile();
        genbankFile.setId(1L);
        genbankFile.setName(FILE_GB);
        mutation.setGenbankFile(genbankFile);
        Gene gene = new Gene();
        gene.setSymbol(SYMBOL);
        mutation.setGenes(new HashSet<>(Collections.singletonList(gene)));
        Outcome outcome = new Outcome();
        outcome.setTpo(TPO);
        mutation.setOutcomes(new HashSet<>(Collections.singletonList(outcome)));
        MutationCategorization mutationCategorization = new MutationCategorization();
        mutationCategorization.setName(MUTATION_CATEGORIZATION);
        mutation.setMutationCategorizations(
            new HashSet<>(Collections.singletonList(mutationCategorization)));
        return mutation;
    }

    @Test
    void toEntity()
    {
        MutationCommonDTO mutationCommonDTO = buildMutationDto();

        Mutation mutation = testInstance.toEntity(mutationCommonDTO);

        assertThat(
            mutation.getMgiAlleleSymbolRequiresConstruction(),
            is(MGI_ALLELE_SYMBOL_REQUIRES_CONSTRUCTION));
        assertThat(mutation.getAlleleConfirmed(), is(ALLELE_CONFIRMED));
        assertThat(mutation.getImitsAllele(), is(IMITS_ALLELE));
        assertThat(mutation.getGeneticMutationType().getName(), is(GENETIC_MUTATION_TYPE));
        assertThat(mutation.getMolecularMutationType().getName(), is(MOLECULAR_MUTATION_TYPE));
        assertThat(mutation.getGenbankFile().getName(), is(FILE_GB));

        verify(mutationQCResultMapper, times(1)).toEntities(mutationCommonDTO.getMutationQCResultDTOs());
        verify(geneMapper, times(1)).toEntities(mutationCommonDTO.getGeneDTOS());
        verify(mutationSequenceMapper, times(1)).toEntities(mutationCommonDTO.getMutationSequenceDTOS());
        verify(mutationCategorizationMapper, times(1)).toEntities(mutationCommonDTO.getMutationCategorizationDTOS());
    }

    private MutationCommonDTO buildMutationDto()
    {
        MutationCommonDTO mutationCommonDTO = new MutationCommonDTO();
        mutationCommonDTO.setMgiAlleleSymbolRequiresConstruction(MGI_ALLELE_SYMBOL_REQUIRES_CONSTRUCTION);
        mutationCommonDTO.setAlleleConfirmed(ALLELE_CONFIRMED);
        mutationCommonDTO.setImitsAllele(IMITS_ALLELE);
        mutationCommonDTO.setGeneticMutationTypeName(GENETIC_MUTATION_TYPE);
        mutationCommonDTO.setMolecularMutationTypeName(MOLECULAR_MUTATION_TYPE);
        mutationCommonDTO.setGenbankFileName(FILE_GB);
        mutationCommonDTO.setGeneDTOS(new ArrayList<>());
        mutationCommonDTO.setMutationCategorizationDTOS(new ArrayList<>());
        mutationCommonDTO.setMutationSequenceDTOS(new ArrayList<>());
        mutationCommonDTO.setMutationQCResultDTOs(new ArrayList<>());
        return mutationCommonDTO;
    }
}