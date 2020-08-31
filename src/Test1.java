import java.util.concurrent.PriorityBlockingQueue;

/*
*
*                   定时器
*
* */
public class Test1 {



    //优先队列中的元素必须是可比较的
    //比较规则的指定主要是两种方式
    //1.让Task实现Comparable接口
    //2.让优先级队列构造的时候,传入一个比较器对象(Comparator)
    static class Task implements Comparable<Task>{
        //Runnable 中有一个run方法,就可以借助这个run方法来描述要执行的具体任务是啥
        private Runnable command;
        //time 表示啥时候来执行command,是一个绝对时间(ms级别的时间戳)
        private long time;

        //构造方法的参数表示:多少毫秒之后执行(相对时间)
        public Task(Runnable command, long after) {
            this.command = command;
            this.time = System.currentTimeMillis() + after;
        }

        //执行任务的具体逻辑
        public void run(){
            command.run();
        }

        @Override
        public int compareTo(Task o) {
            //谁的时间小先执行谁
            return (int) (this.time - o.time);
        }
    }



    //扫描线程的逻辑
    static class Worker extends Thread{
        private PriorityBlockingQueue<Task> queue = null;
        private Object mailBox = null;

        public Worker(PriorityBlockingQueue<Task> queue,Object mailBox){
            this.queue = queue;
            this.mailBox  = mailBox;
        }

        @Override
        public void run() {
            //实现具体的线程执行内容
            while (true) {
                try {
                    //1.取出队首元素,检查时间是否到
                    Task task = queue.take();
                    //2.检查当前任务时间是否到了
                    long curTime = System.currentTimeMillis();
                    if (task.time > curTime){
                        //时间还没到,就把当前任务再放回队列中
                        queue.put(task);
                        //解决忙等,扫描线程内部加wait
                        synchronized (mailBox){
                            //等待的是相差的时间
                            mailBox.wait(task.time - curTime);
                        }
                    }else {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    static class Timer{
        //为了避免忙等,需要使用wait方法
        //使用一个单独的对象辅助进行wait
        //使用this也可以
        private Object mailBox = new Object();

        //定时器的基本构成,有三部分
        //1.用一个类来描述"任务"
        //2.用一个阻塞优先队列来组织若干个任务,让队首元素就是时间最早的任务,
        // 如果队首元素时间未到,那么其他元素也肯定比態执行
        private PriorityBlockingQueue<Task>  queue = new PriorityBlockingQueue<>();
        //3.用一个线程来循环扫描当前阻塞优先队列的队首元素,如果时间到,就执行指定的任务
        public Timer(){
            Worker worker = new Worker(queue,mailBox);
            worker.start();
        }
        //4.还需要提供一个方法,让调用者能把任务给安排进去
        public void schedule(Runnable command, long after){
            Task task = new Task(command, after);
            queue.put(task);
            //解决忙等 在安排方法中加上notify
            synchronized (mailBox){
                mailBox.notify();
            }
        }
    }


    public static void main(String[] args) {
        Timer timer = new Timer();
        timer.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("hehe");
                timer.schedule(this,2000);
            }
        },5000);
    }

}
