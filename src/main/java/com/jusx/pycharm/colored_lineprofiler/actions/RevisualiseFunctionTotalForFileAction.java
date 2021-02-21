package com.jusx.pycharm.colored_lineprofiler.actions;

import com.jusx.pycharm.colored_lineprofiler.service.TimeFractionCalculation;


public class RevisualiseFunctionTotalForFileAction extends RevisualiseProfileTotalForFileAction {
    @Override
    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.FUNCTION_TOTAL;
    }
}
