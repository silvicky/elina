package io.silvicky.elina.webmap.subway;

import java.util.HashMap;
import java.util.Map;

public class Subway
{
    public final Map<String,SubwayLine> lines=new HashMap<>();
    public final Map<String,SubwayStation> stationDetails=new HashMap<>();
}
