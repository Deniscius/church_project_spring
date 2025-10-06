import React from 'react';
import ReactDOM from 'react-dom';
// import { IoMdClose } from "react-icons/io";

const Modal = ({ isOpen, onClose, children }) => {
    if (!isOpen) return null;

    return ReactDOM.createPortal(
        <>
            <div className="fixed inset-0 bg-black bg-opacity-75 z-50" onClick={onClose} />
            <div className="fixed inset-1/2 bg-grey transform -translate-x-1/2 -translate-y-1/2 p-6 rounded-md z-50 flex flex-col gap-5 xl:min-w-[400px] min-w-[300px] h-fit">
                <div className="flex justify-end items-center">
                    {/* <IoMdClose className='text-[26px] cursor-pointer' onClick={onClose} /> */}
                </div>
                {children}
            </div>
        </>,
        document.body
    );
};

export default Modal;