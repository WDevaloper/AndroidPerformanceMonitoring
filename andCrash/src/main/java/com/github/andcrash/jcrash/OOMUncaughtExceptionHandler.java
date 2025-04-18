package com.github.andcrash.jcrash;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

import com.github.andcrash.hprofparser.Hprof;
import com.github.andcrash.hprofparser.HprofKt;
import com.kwai.koom.base.DefaultInitTask;
import com.kwai.koom.fastdump.ForkJvmHeapDumper;

import java.io.File;
import java.io.IOException;

public class OOMUncaughtExceptionHandler implements IUncaughtExceptionHandler {
    @Override
    public void uncaughtException(Context context, String logFir, Thread thread, Throwable ex) throws IOException {
        try {
            File hprofFile = new File(logFir, "dump_" + System.currentTimeMillis() + ".hprof");
            Debug.dumpHprofData(hprofFile.getAbsolutePath());
//            Hprof hprof = HprofKt.hprofParse(hprofFile.getAbsolutePath());
//            Log.e("AndCrash", "hprof:" + hprof);
            ForkJvmHeapDumper.getInstance().dump(hprofFile.getAbsolutePath());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean handleCrashAfter(Context context) {
        return false;
    }

    @Override
    public boolean isHandledable(Throwable ex) {
        return ex instanceof OutOfMemoryError;
    }

}
