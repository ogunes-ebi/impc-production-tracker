package org.gentar.biology.ortholog;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.gentar.biology.project.projection.dto.ProjectSearchDownloadOrthologDto;
import org.gentar.graphql.GraphQLConsumer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.client.RestTemplate;

@Component
public class OrthologServiceImpl implements OrthologService {

    public static final String LOCALHOST_ORTHOLOG_API_URL =
        "http://localhost:8090/api/ortholog/find_one_to_many_by_mgi_ids?mgiIds=";

    public static final String GENTAR_ORTHOLOG_API_URL =
        "https://www.gentar.org/orthology-api/api/ortholog/find_all_by_mgi_ids?mgiIds=";

    public static final String ORTHOLOG_API_URL =
        "http://api-ortholog-service-reference-db.mi-reference-data.svc.cluster.local:8080/orthology-api/api/ortholog/find_all_by_mgi_ids?mgiIds=";

    public final int CHUNK_SIZE = 100;
    private final GraphQLConsumer graphQLConsumer;
    private final JSONToOrthologsMapper jsonToOrthologsMapper;
    private static Logger LOGGER = Logger.getLogger("InfoLogging");
    private static final int THRESHOLD_SUPPORT_COUNT = 4;

    static final String ORTHOLOGS_BY_ACC_ID_QUERY =
        "{ \"query\":" +
            " \"{ " +
            "%s" +
            "}\" " +
            "}";
    static final String ORTHOLOGS_BODY_QUERY =
        " %s: mouse_gene(where:" +
            " { mgi_gene_acc_id: {_eq: \\\"%s\\\"}}) {" +
            "   orthologs {" +
            "     human_gene {" +
            "       symbol" +
            "       hgnc_acc_id" +
            "     }" +
            "     category" +
            "     support" +
            "     support_count" +
            "   }" +
            "   mgi_gene_acc_id" +
            "   symbol" +
            " }";

    public OrthologServiceImpl(GraphQLConsumer graphQLConsumer,
                               JSONToOrthologsMapper jsonToOrthologsMapper) {
        this.graphQLConsumer = graphQLConsumer;
        this.jsonToOrthologsMapper = jsonToOrthologsMapper;
    }

    @Override
    public Map<String, List<Ortholog>> getOrthologsByAccIds(List<String> accIds) {
        Map<String, List<Ortholog>> orthologs = new HashMap<>();
        if (accIds != null && !accIds.isEmpty()) {
            String query = buildQuery(accIds);
            String result = graphQLConsumer.executeQuery(query);
            orthologs = jsonToOrthologsMapper.toOrthologs(result);
        }
        return orthologs;
    }

    private String buildQuery(List<String> accIds) {
        String query = "";
        AtomicInteger counter = new AtomicInteger();
        StringBuilder builder = new StringBuilder();
        accIds.forEach(x -> {
            String subQueryName = "query" + counter.getAndIncrement();
            builder.append(String.format(ORTHOLOGS_BODY_QUERY, subQueryName, x));
        });
        query = String.format(ORTHOLOGS_BY_ACC_ID_QUERY, builder.toString());
        return query;
    }

    public List<Ortholog> calculateBestOrthologs(List<Ortholog> orthologs) {
        List<Ortholog> bestOrthologs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orthologs)) {
            Map<Integer, List<Ortholog>> mappedBySupportCount = new HashMap<>();
            orthologs.forEach(x -> {
                List<Ortholog> elementsWithSameCount =
                    mappedBySupportCount.get(x.getSupportCount());
                if (elementsWithSameCount == null) {
                    elementsWithSameCount = new ArrayList<>();
                }
                elementsWithSameCount.add(x);
                mappedBySupportCount.put(x.getSupportCount(), elementsWithSameCount);
            });
            Set<Integer> keys = mappedBySupportCount.keySet();
            Integer max = Collections.max(keys);
            if (max > THRESHOLD_SUPPORT_COUNT) {
                bestOrthologs = mappedBySupportCount.get(max);
            }
        }
        return bestOrthologs;
    }

    @Override
    @Cacheable("mgiIds")
    public List<ProjectSearchDownloadOrthologDto> getOrthologs(List<String> mgiIds) {

        LOGGER.info("ortholog caching started");

        final Collection<List<String>> mgiChunks = groupMgiIdsToChunks(mgiIds);

        List<ProjectSearchDownloadOrthologDto> downloadOrthologDtos =
            new ArrayList<>();

        mgiChunks.forEach(mgiChunk -> {

            final String harlowOrthologUri =
                ORTHOLOG_API_URL +
                    String.join(",", mgiChunk);

            ResponseEntity<ProjectSearchDownloadOrthologDto[]> response =
                new RestTemplate().getForEntity(
                    harlowOrthologUri,
                    ProjectSearchDownloadOrthologDto[].class);

            downloadOrthologDtos.addAll(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        });

        List<ProjectSearchDownloadOrthologDto>
            calculatedOrthologDtos = calculateBestSearchDownloadOrthologs(downloadOrthologDtos);

        List<ProjectSearchDownloadOrthologDto>
            sortedDownloadOrthologDtos = sortDownloadOrthologDtos(calculatedOrthologDtos);

        LOGGER.info("ortholog caching ended");
        return sortedDownloadOrthologDtos;

    }

    private List<ProjectSearchDownloadOrthologDto> sortDownloadOrthologDtos(
        List<ProjectSearchDownloadOrthologDto> downloadOrthologDtos) {
        List<ProjectSearchDownloadOrthologDto> sortedDownloadOrthologDtos =
            downloadOrthologDtos.stream()
                .sorted(Comparator.comparing(ProjectSearchDownloadOrthologDto::getMgiGeneAccId))
                .collect(
                    Collectors.toList());
        return sortedDownloadOrthologDtos;
    }

    private Collection<List<String>> groupMgiIdsToChunks(List<String> mgiIds) {

        final AtomicInteger counter = new AtomicInteger();

        return mgiIds.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / CHUNK_SIZE))
            .values();
    }


    public List<ProjectSearchDownloadOrthologDto> calculateBestSearchDownloadOrthologs(
        List<ProjectSearchDownloadOrthologDto> orthologs) {

        List<ProjectSearchDownloadOrthologDto> bestOrthologs = new ArrayList<>(orthologs);

        orthologs.forEach(x -> {
            if (bestOrthologs.stream().filter(y -> y.getMgiGeneAccId().equals(x.getMgiGeneAccId()))
                .count() > 1) {
                ProjectSearchDownloadOrthologDto bestOrtholog =
                    getBestOrthologs(bestOrthologs, x);

                List<ProjectSearchDownloadOrthologDto>
                    bestOrthologSameSupportCount =
                    getBestOrthologWithSameSupportCount(bestOrthologs, x, bestOrtholog);

                removeDuplicatedOrthologs(bestOrthologs, x);

                addDuplicatedOrthologs(bestOrthologs, bestOrtholog, bestOrthologSameSupportCount);
            }

        });
        return bestOrthologs;
    }

    private void addDuplicatedOrthologs(List<ProjectSearchDownloadOrthologDto> bestOrthologs,
                                        ProjectSearchDownloadOrthologDto bestOrtholog,
                                        List<ProjectSearchDownloadOrthologDto> bestOrthologSameSupportCount) {
        if (bestOrthologSameSupportCount.size() > 1) {
            String humanGenSymbol = bestOrthologSameSupportCount.stream().map(
                ProjectSearchDownloadOrthologDto::getHumanGeneSymbol).collect(
                Collectors.joining(":"));
            bestOrtholog.setHumanGeneSymbol(humanGenSymbol);
            bestOrthologs.add(bestOrtholog);
        } else {
            bestOrthologs.add(bestOrtholog);
        }
    }

    private void removeDuplicatedOrthologs(List<ProjectSearchDownloadOrthologDto> bestOrthologs,
                                           ProjectSearchDownloadOrthologDto x) {
        bestOrthologs.removeAll(bestOrthologs.stream().filter(
            y -> y.getMgiGeneAccId().equals(x.getMgiGeneAccId())).collect(
            Collectors.toList()));
    }

    private List<ProjectSearchDownloadOrthologDto> getBestOrthologWithSameSupportCount(
        List<ProjectSearchDownloadOrthologDto> bestOrthologs, ProjectSearchDownloadOrthologDto x,
        ProjectSearchDownloadOrthologDto bestOrtholog) {
        List<ProjectSearchDownloadOrthologDto> bestOrthologSameSupportCount =
            bestOrthologs.stream().filter(
                y -> y.getMgiGeneAccId().equals(x.getMgiGeneAccId()) &&
                    bestOrtholog.getSupportCount().equals(y.getSupportCount())).collect(
                Collectors.toList());
        return bestOrthologSameSupportCount;
    }

    private ProjectSearchDownloadOrthologDto getBestOrthologs(
        List<ProjectSearchDownloadOrthologDto> bestOrthologs, ProjectSearchDownloadOrthologDto x) {
        ProjectSearchDownloadOrthologDto bestOrtholog = bestOrthologs.stream()
            .filter(y -> y.getMgiGeneAccId().equals(x.getMgiGeneAccId())).collect(
                Collectors.toList()).stream()
            .max(Comparator.comparing(ProjectSearchDownloadOrthologDto::getSupportCount))
            .orElseThrow(NoSuchElementException::new);
        return bestOrtholog;
    }

}
