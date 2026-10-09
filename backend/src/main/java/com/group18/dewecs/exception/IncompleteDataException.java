package com.group18.dewecs.exception;

import java.util.List;

/**
 * The report cannot be generated yet because baseline data is missing. The officer must choose to generate a
 * provisional draft or to override with a justification. Handled by the report page, not by the global handler.
 */
public class IncompleteDataException extends RuntimeException {

    private final List<String> gaps;

    public IncompleteDataException(List<String> gaps) {
        super("Incomplete data: " + gaps.size() + " gap(s) found.");
        this.gaps = gaps;
    }

    public List<String> getGaps() {
        return gaps;
    }
}
