package nl.jusx.pycharm.lineprofiler.profile;


public class ProfileSchema {
    static class Function {
        static class Line {
            int lineNo;
            long hits;
            long time;

        }

        String file;
        int lineNo;
        String functionName;
        Function.Line[] profiledLines;
    }

    Function[] profiledFunctions;
    float unit;
}



