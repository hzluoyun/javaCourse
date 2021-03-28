## 1.GC的背景及基本原理
**核心观点**
1）GC的出现本质上是内存资源的局限性
2）Java的内存管理实际上就是对象的管理，其中包括对象的分配和释放。
**java内存模型**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328182531607.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
3）线程栈的内存分配在编译期大致可知，这部分内确定性，无需考虑如何回收
4）Java的堆和方法区（从Java8开始被Metaspace代替）有着很显著的不确定性，这部分是在运行时动态变化的，这部分的内存分配和回收是动态的，堆中存放的就是对象，需要对于已经死亡的对象进行清理和回收，以保证腾出空间给其他对象使用，这就是GC干的活

## 2 GC的典型的几种算法
**首先来看下是堆内存结构**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328183624453.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
1）堆内存是所有线程共用的内存空间，JVM 将Heap 内存分为年轻代（Young generation）和 老年代（Old generation, 也叫 Tenured）两部分。
2）年轻代还划分为 3 个内存池，新生代（Edenspace）和存活区（Survivor space）, 在大部分GC 算法中有 2 个存活区（S0, S1），在我们可以观察到的任何时刻，S0 和 S1 总有一个是空的, 但一般较小，也不浪费多少空间。Non-Heap 本质上还是 Heap，只是一般不归 GC
管理，里面划分为 3 个内存池。Metaspace, 以前叫持久代（永久代, Permanentgeneration）, Java8 换了个名字叫 Metaspace。
**GC的算法演进以及基于堆内存划分的不同应用策略**
引用计数法
如果一个对象被引用了一次计数值就加1，使用完毕，计数值减一。当一个对象的计数值为0时，就意味着它不再被使用，就可以将其清理掉。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328195555879.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
这种算法存在的问题：循环依赖
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328195643127.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
算法演进，解决引用计数-循环依赖的问题

标记清除算法
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328195818582.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
Marking（标记）: 遍历所有的可达对象，并在本地内存(native)中分门别类记下。
Sweeping（清除）: 这一步保证了，不可达对象所占用的内存，在之后进行内存分配时可以重用。

除了清除，有的标记清除算法还需要做压缩，说白了就是将零星使用的内存变得连续紧凑
![在这里插入图片描述](https://img-blog.csdnimg.cn/2021032819593457.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
怎么才能标记和清除清楚上百万对象呢？
答案就是 **STW**，让全世界停止下来。（Stop The World）
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328200317192.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)

针对GC的三种典型算法：标记-复制、标记-清除、标记-清除-整理
对应于堆内存结构中，如何适应于不同的堆区？
年轻代发生的的GC叫做Young GC（YGC），频率比较高，速度也比较快。
老年代存活的时间比较长，GC频率比较低，只有等到为堆整体做一次GC（Full GC，简称FGC）的时候，才会为Old区做GC。
新生代的对象转移到存活区使用的是复制，也就是采用的标记-复制算法

老年代只有一个区，默认都是存活对象，不能复制以后进行暴力删除。整理老年代的空间中的内容（相当于是进行压缩），是将所有存活对象移动到老年代的开始位置进行存放，目的是不让内存使用碎片化。
也就是上面提到的标记-清除-整理算法。
## 3 GC的执行策略
**串行GC**
-XX:+UseSerialGC 配置串行 GC
串行 GC 对年轻代使用 mark-copy（标记-复制） 算法，对老年代使用 mark-sweep-compact（标记-清除整理）算法

**并行GC**
-XX:+UseParallelGC
-XX:+UseParallelOldGC
-XX:+UseParallelGC -XX:+UseParallelOldGC
年轻代和老年代的垃圾回收都会触发 STW 事件。
在年轻代使用 标记-复制（mark-copy）算法，在老年代使用 标记-清除-整理（mark-sweepcompact）算法。

**CMS GC（Mostly Concurrent Mark and Sweep
Garbage Collector）**
-XX:+UseConcMarkSweepGC
其对年轻代采用并行 STW 方式的 mark-copy (标记-复制)算法，对老年代主要使用并发 mark-sweep (
标记-清除)算法。
CMS GC 的设计目标是避免在老年代垃圾收集时出现长时间的卡顿

**G1 GC**
G1 的全称是 Garbage-First，意为垃圾优先，哪一块的垃圾最
多就优先清理它。
G1 GC 最主要的设计目标是：将 STW 停顿的时间和分布，变成
可预期且可配置的


## 4 GC配置策略对比分析
串行GC与并行GC对比，经过分析并行GC平均时间更短，但是发生的GC次数更多，总的GC时间并行大于串行（这样来看不是并行GC
效率更低下了），这个是否是合理；
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210328203935511.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0xqdW4wNTEz,size_16,color_FFFFFF,t_70)
