package com.jusx.pycharm.colored_lineprofiler.profile;


public class ProfileSchema {
    static class Function {
        static class Line {
            int lineNo;
            int hits;
            int time;

        }

        String file;
        int lineNo;
        String functionName;
        Function.Line[] profiledLines;
    }

    Function[] profiledFunctions;
    float unit;
}



