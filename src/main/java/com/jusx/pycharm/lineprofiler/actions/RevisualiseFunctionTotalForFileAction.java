package com.jusx.pycharm.lineprofiler.actions;

import com.jusx.pycharm.lineprofiler.service.TimeFractionCalculation;


public class RevisualiseFunctionTotalForFileAction extends RevisualiseProfileTotalForFileAction {
    @Override
    protected TimeFractionCalculation withTimeFractionCalculation() {
        return TimeFractionCalculation.FUNCTION_TOTAL;
    }
}
