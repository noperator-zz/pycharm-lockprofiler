package com.jusx.pycharm.colored_lineprofiler.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.jusx.pycharm.colored_lineprofiler.service.TimeFractionCalculation;

public class VisualiseLineProfilerFunctionTotalAction extends VisualiseLineProfilerAction {
    private static final Logger logger = Logger.getInstance(VisualiseLineProfilerFunctionTotalAction.class.getName());

    @Override
    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.FUNCTION_TOTAL;
    }
}
