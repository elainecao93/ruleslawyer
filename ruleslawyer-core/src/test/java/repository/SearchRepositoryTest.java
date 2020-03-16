package repository;

import contract.SearchResult;
import contract.rules.AbstractRule;
import contract.rules.RuleHeader;
import contract.searchRequests.RuleSearchRequest;
import org.junit.Test;
import repository.SearchRepository;
import utils.TestUtils;

import java.util.List;

import static contract.RequestSource.DISCORD;
import static contract.RuleSource.CR;
import static contract.RuleSource.IPG;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SearchRepositoryTest {

    private RuleHeader cheatingRule = TestUtils.getSampleIPGRule();
    private RuleHeader layersRule = TestUtils.getSampleCRRule();
    private SearchRepository<AbstractRule> ruleSearchRepository = new SearchRepository<>(asList(cheatingRule, layersRule), DISCORD);

    @Test
    public void searchRules_IPG_EntireHeader_ExpectCorrectResults() {
        RuleSearchRequest ruleSearchRequest = new RuleSearchRequest(singletonList("cheating"), IPG, 1);

        List<SearchResult<AbstractRule>> output = ruleSearchRepository.getSearchResult(ruleSearchRequest);

        assertThat(output.size(), is(1));
        assertThat(output.get(0).getEntry(), is(cheatingRule));
    }

    @Test
    public void searchRules_IPG_SomeRules_ExpectCorrectResults() {
        RuleSearchRequest ruleSearchRequest = new RuleSearchRequest(singletonList("lies"), IPG, 1);

        List<SearchResult<AbstractRule>> output = ruleSearchRepository.getSearchResult(ruleSearchRequest);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getEntry().getText(), is("A person breaks a rule defined by the tournament documents, lies to a Tournament Official, or notices an offense committed in their (or a teammate’s) match and does not call attention to it."));
        assertThat(output.get(1).getEntry().getText(), is("B. A player lies to a tournament official about what happened in a game to make their case stronger."));
    }

    @Test
    public void searchRules_CR_SomeRule_IncludingSubRulesOfIncludedRule_ExpectOnlyTopLevelResults() {
        RuleSearchRequest ruleSearchRequest = new RuleSearchRequest(singletonList("layer 7"), CR, 1);

        List<SearchResult<AbstractRule>> output = ruleSearchRepository.getSearchResult(ruleSearchRequest);

        assertThat(output.size(), is(2));
        assertThat(output.get(0).getEntry().getText(), is("613.3 Within layer 7, apply effects in a series of sublayers in the order described below. Within each sublayer, apply effects in timestamp order. (See rule 613.6.) Note that dependency may alter the order in which effects are applied within a sublayer. (See rule 613.7.)"));
        assertThat(output.get(1).getEntry().getText(), is("613.1g Layer 7: Power- and/or toughness-changing effects are applied."));
    }
}