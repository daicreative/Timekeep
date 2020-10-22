package com.dai.timekeep;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class AllocatePagerAdapater extends FragmentPagerAdapter {

    public AllocatePagerAdapater(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new AllocateWheelFragment();
            case 1:
                return new AllocateTypeFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }


}
