package org.netty.firstexample;


import java.util.List;
import java.util.concurrent.*;


// AllMgr继承Runnable接口
public class AllMgr implements Runnable {

    // 有一个线程池
    private final ExecutorService executor;

    // 有一个成员变量LinkedBlockingQueue<MgrParent>可以弹出Mgr对象
    private final LinkedBlockingQueue<MgrParent> queue;

    // 有一个running标志
    private volatile boolean running;

    // 构造方法，初始化线程池和队列
    public AllMgr(int poolSize) {
        executor = Executors.newFixedThreadPool(poolSize);
        queue = new LinkedBlockingQueue<>();
        running = true;
    }

    // 实现的run()方法有while循环，while(running)
    @Override
    public void run() {
        while (running) {
            try {
                // 弹出操作要加锁
                synchronized (queue) {
                    // 从队列中弹出一个Mgr对象
                    MgrParent mgr = queue.take();
                    // 提交给线程池执行
                    executor.execute(mgr);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 停止运行的方法
    public void stop() {
        running = false;
        executor.shutdown();
    }

    // 向队列中添加Mgr对象的方法，不能重复添加
    public void add(MgrParent mgr) {
        if (!queue.contains(mgr)) {
            queue.offer(mgr);
        }
    }
}

// MgrParent是GamePlayer的间接父类
abstract class MgrParent implements Runnable {

    // 有一个成员变量AllMgr
    protected AllMgr allMgr;

    // 有一个List，List存的是提交的任务
    protected List<Runnable> tasks;

    // 构造方法，初始化AllMgr和List
    public MgrParent(AllMgr allMgr) {
        this.allMgr = allMgr;
        tasks = new CopyOnWriteArrayList<>();
    }

    // 实现的run()方法会执行List中的所有任务
    @Override
    public void run() {
        for (Runnable task : tasks) {
            task.run();
        }
        tasks.clear();
    }

    // 向List中添加任务的方法
    public void addTask(Runnable task) {
        tasks.add(task);
    }
}

// GamePlayer继承MgrParent
class GamePlayer extends MgrParent {

    // 构造方法，调用父类构造方法
    public GamePlayer(AllMgr allMgr) {
        super(allMgr);
    }

    // 当调用GamePlayer的PushTask(f())方法时，会将任务提交到List，并将自身提交到AllMgr的队列中
    public void PushTask(Runnable f) {
        addTask(f);
        allMgr.add(this);
    }
}
