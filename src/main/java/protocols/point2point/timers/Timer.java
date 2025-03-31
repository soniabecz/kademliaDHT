package protocols.point2point.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class Timer extends ProtoTimer {

    public static final short TIMER_ID = 402;

    public Timer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}

