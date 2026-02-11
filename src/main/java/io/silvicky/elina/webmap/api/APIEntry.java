package io.silvicky.elina.webmap.api;

import static io.silvicky.elina.Elina.LOGGER;
public class APIEntry
{
    private static void registerBlueMap()
    {
        try
        {
            Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
        }
        catch(Exception e)
        {
            LOGGER.warn("BlueMap not found");
            return;
        }
        BlueMap.register();
    }
    public static void register()
    {
        registerBlueMap();
    }
    private static void refreshBlueMap()
    {
        try
        {
            Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
        }
        catch(Exception e)
        {
            return;
        }
        BlueMap.refresh();
    }
    public static void refresh()
    {
        refreshBlueMap();
    }
}
