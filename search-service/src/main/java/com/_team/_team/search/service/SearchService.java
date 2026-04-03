package com._team._team.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com._team._team.search.domain.MemberDocument;
import com._team._team.search.domain.OrganizationDocument;
import com._team._team.search.dto.resdto.MemberSearchResDto;
import com._team._team.search.repository.MemberSearchRepository;
import com._team._team.search.repository.OrganizationSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final MemberSearchRepository memberSearchRepository;
    private final OrganizationSearchRepository organizationSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;

    // 직원 통합 검색
    public Page<MemberSearchResDto> searchEmployees(
            String query, String companyId, Pageable pageable) {

        Query searchQuery = buildEmployeeSearchQuery(
                query, companyId, pageable);

        SearchHits<MemberDocument> searchHits =
                elasticsearchOperations.search(
                        searchQuery, MemberDocument.class);

        return SearchHitSupport
                .searchPageFor(searchHits, pageable)
                .map(hit -> MemberSearchResDto
                        .fromDocument(hit.getContent()));
    }

    // 조직 트리 조회 (정렬 추가)
    public List<OrganizationDocument> getOrganizationTree(
            String companyId) {
        return organizationSearchRepository
                .findByCompanyIdOrderByLabelAsc(companyId);
    }

    // 조직별 직원 검색 (nested 쿼리)
    public Page<MemberSearchResDto> searchEmployeesByOrganization(
            String organizationId, UUID companyId, Pageable pageable) {

        Query searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .must(s -> s.nested(n -> n
                                .path("organizationList")
                                .query(nq -> nq.term(t -> t
                                        .field("organizationList.organizationId")
                                        .value(organizationId)))))
                        .filter(f -> f.term(t -> t
                                .field("companyId")
                                .value(companyId.toString())))))
                .withPageable(pageable)
                .build();

        SearchHits<MemberDocument> searchHits =
                elasticsearchOperations.search(
                        searchQuery, MemberDocument.class);

        return SearchHitSupport
                .searchPageFor(searchHits, pageable)
                .map(hit -> MemberSearchResDto
                        .fromDocument(hit.getContent()));
    }

    // 검색 쿼리 빌더
    private Query buildEmployeeSearchQuery(
            String query, String companyId, Pageable pageable) {

        // 전화번호 검색 (숫자/하이픈만 있는 경우)
        if (query.matches("^[0-9\\-]+$")) {
            return new NativeQueryBuilder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t
                                    .field("phoneNumber")
                                    .value(query)))
                            .filter(f -> f.term(t -> t
                                    .field("companyId")
                                    .value(companyId)))))
                    .withPageable(pageable)
                    .build();
        }

        // 일반 검색 (이름, 직책, 이메일, 조직)
        return new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .should(s -> s.multiMatch(mm -> mm
                                .query(query)
                                .fields("name", "titleName",
                                        "email", "position")))
                        .should(s -> s.nested(n -> n
                                .path("organizationList")
                                .query(nq -> nq.match(m -> m
                                        .field("organizationList.name")
                                        .query(query)))))
                        .minimumShouldMatch("1")
                        .filter(f -> f.term(t -> t
                                .field("companyId")
                                .value(companyId)))))
                .withPageable(pageable)
                .build();
    }
}