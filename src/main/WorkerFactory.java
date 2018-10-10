package main;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class WorkerFactory extends BasePooledObjectFactory<TreeWorker> {

    @Override
    public TreeWorker create() {
        return new TreeWorker();
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
