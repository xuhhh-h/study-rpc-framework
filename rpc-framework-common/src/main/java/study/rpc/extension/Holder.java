package study.rpc.extension;

public class Holder<T> {
    //volatile是一种同步机制，告诉 JVM 和线程，在多个线程之间共享这个变量时，任何线程对它的修改都会立即对其他线程可见。
    //volatile 不会保证原子性。它只是保证当一个线程修改该变量时，其他线程能够立即看到修改后的值。
    //synchronized用于确保原子性和可见性。它通过锁定对象来确保同一时间只有一个线程可以访问被 synchronized 修饰的代码块或方法。
    //volatile 相对于 synchronized 性能更好，因为它不涉及锁的开销。它适合用于单一变量的共享，尤其是变量不涉及复杂的计算时。
    private volatile T value;

    public T get(){
        return value;
    }

    public void set(T value){
        this.value = value;
    }
}
