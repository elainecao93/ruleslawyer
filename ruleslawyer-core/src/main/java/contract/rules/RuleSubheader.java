package contract.rules;

import contract.RuleSource;

import java.util.ArrayList;

import static java.util.stream.Collectors.joining;

public class RuleSubheader extends AbstractRule {

    public RuleSubheader(String text) {
        this.text = text;
        this.subRules = new ArrayList<>();
        this.index = ++ruleCount;
    }

    @Override
    public RuleSource getRuleSource() {
        return parentRule.getRuleSource();
    }

    public RuleHeader getHeader() {
        return (RuleHeader)this.parentRule;
    }
}