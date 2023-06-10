package lin.xposed.hook.item;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.CountDownTimer;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import lin.xposed.ReflectUtils.MethodUtils;
import lin.xposed.ReflectUtils.PostMain;
import lin.xposed.hook.util.XPBridge;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class HookSensorEventListener {
    private static final long TOTAL_TIME = 1000 * 10;
    private static final long ON_TICK_TIME = 1000;
    private static final AtomicBoolean startActivityHasEnd = new AtomicBoolean();
    /**
     * CountDownTimer 实现倒计时
     */
    private final CountDownTimer countDownTimer = new CountDownTimer(TOTAL_TIME/*总时长*/,
            ON_TICK_TIME/*触发onTick的间隔*/) {

        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            //10秒后启动页结束不再继续拦截
            startActivityHasEnd.set(true);
        }
    };

    public void startHook() {
        countDownTimer.start();

        Method allClassMethod = MethodUtils.findMethod(ClassLoader.class, "loadClass", Class.class, new Class[]{String.class});

        final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        XposedBridge.hookMethod(allClassMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Class<?> clz = (Class<?>) param.getResult();
                if (clz == null) return;
                //获取类实现的接口列表 因为没法直接hook接口的抽象方法
                Class<?>[] interfacesList = clz.getInterfaces();
                if (interfacesList != null) {
                    for (Class<?> interfaces : interfacesList) {
                        //判断该类是否实现了这个接口
                        if (interfaces == SensorEventListener.class) {
                            Method onSensorChanged = MethodUtils.findUnknownReturnMethod(clz, "onSensorChanged", new Class[]{SensorEvent.class});
                            XPBridge.hookBefore(onSensorChanged, onSensorParam -> {
                                //如果启动页结束了就不再拦截了
                                if (!startActivityHasEnd.get()) {

                                    if (atomicBoolean.getAndSet(false)) {
                                        PostMain.postMain(() -> {
//                                                Toast.makeText(HookEnv.getHostAppContext(), "已禁止摇一摇跳转", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                    //在beforeHookedMethod时使用setResult则不会执行剩余的代码
                                    onSensorParam.setResult(null);
                                }
                            });
                        }
                    }
                }
            }
        });

    }

}
