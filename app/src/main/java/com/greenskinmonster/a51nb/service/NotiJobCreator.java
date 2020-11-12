package com.greenskinmonster.a51nb.service;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by GreenSkinMonster on 2017-07-19.
 */

public class NotiJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        if (NotiJob.TAG.equals(tag))
            return new NotiJob();

        return null;
    }
}
