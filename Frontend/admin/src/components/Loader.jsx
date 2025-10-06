import React from 'react';

const Loader = () => {

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black opacity-90 z-50">
            <div className="w-16 h-16 border-8 border-white z-51 border-t-transparent rounded-full animate-spin">
            </div>
        </div>
    );
};

export default Loader;
