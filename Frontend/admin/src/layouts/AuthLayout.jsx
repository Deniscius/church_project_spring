import React from 'react';
import { Outlet } from 'react-router-dom';

const AuthLayout = () => {
    return (
        <div className='flex w-screen h-screen justify-center items-center bg-grey'>

            <div className="flex justify-center items-center rounded-xl xl:min-w-[300px] w-full">
                <Outlet />
            </div>

        </div>
    )
}

export default AuthLayout