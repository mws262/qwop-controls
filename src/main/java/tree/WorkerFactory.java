package tree;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Object pooling for {@link TreeWorker}. These workers can be checked out and returned between {@link TreeStage}.
 *
 * @author matt
 */
public class WorkerFactory extends BasePooledObjectFactory<TreeWorker> {

    @Override
    public TreeWorker create() {
        return TreeWorker.makeCachedStateTreeWorker(1, 2);
    }

    @Override
    public PooledObject<TreeWorker> wrap(TreeWorker game) {
        return new DefaultPooledObject<>(game);
    }

    /**
     * When an object is returned to the pool, reset it.
     */
    @Override
    public void passivateObject(PooledObject<TreeWorker> gamePoolObject) {
        gamePoolObject.getObject().pauseWorker();
    }

}
