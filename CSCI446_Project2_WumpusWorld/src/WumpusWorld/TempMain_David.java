package WumpusWorld;

public class TempMain_David 
{
    public static void main(String[] args)
    { 
        /*
        * First simple appraoch: test resolution on a simple 3x3 World
        * with only 1 wompus, 2 smelly squares, and nothing else. Make sure
        * method KnowledgeBase.Query() infers cells to correctly be safe or not.
        * Don't worry about exploration just yet. Supply all percepts though
        * the driver
        */
        World world = new World(3);
        
        // hard coding each room state for the moment. Room (0,2) has a wumpus 
        // (this isn't even necessary for initial logic checks but hey why not)
        world.getRoom(0, 0).setAllProperties(false, true, false, false, false, false, false);
        world.getRoom(0, 1).setAllProperties(false, true, false, false, false, true, false);
        world.getRoom(0, 2).setAllProperties(false, false, false, false, false, false, true);
        world.getRoom(1, 0).setAllProperties(false, true, false, false, false, false, false);
        world.getRoom(1, 1).setAllProperties(false, true, false, false, false, false, false);
        world.getRoom(1, 2).setAllProperties(false, true, false, false, false, true, false);
        world.getRoom(2, 0).setAllProperties(false, true, false, false, false, false, false);
        world.getRoom(2, 1).setAllProperties(false, true, false, false, false, false, false);
        world.getRoom(2, 2).setAllProperties(false, true, false, false, false, false, false);
        
        // hard coding the relevant logical sentences
        
    }
}