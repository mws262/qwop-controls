package game;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class GameObjectFactory extends BasePooledObjectFactory<GameLoader>{

	@Override
	public GameLoader create() throws Exception {
		return new GameLoader();
	}

	@Override
	public PooledObject<GameLoader> wrap(GameLoader game) {
		return new DefaultPooledObject<GameLoader>(game);
	}
	
    /**
     * When an object is returned to the pool, reset it.
     */
    @Override
    public void passivateObject(PooledObject<GameLoader> gamePoolObject) {
    	gamePoolObject.getObject().makeNewWorld();
    }

}
