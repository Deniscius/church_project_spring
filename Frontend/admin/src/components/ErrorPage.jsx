import React from 'react';
import { Link } from 'react-router-dom';

const ErrorPage = () => {
    return (
        <div>
            <section className="w-screen h-screen flex justify-center items-center font-agrandir bg-black">
                <div className="w-full">
                    <div className="w-full flex justify-center items-center">
                        <div className="w-full flex justify-center items-center">
                            <div className="text-center w-[60%]">
                                <h1 className="text-center text-6xl text-white">404</h1>
                                <div className="bg-cover bg-center h-[400px]" style={{ backgroundImage: "url(https://cdn.dribbble.com/users/285475/screenshots/2083086/dribbble_1.gif)" }}>
                                </div>

                                <div className="text-white">
                                    <p className="text-4xl pt-5">
                                        Look like you're lost
                                    </p>

                                    <p>La page que vous avez demandé est introuvable !</p>

                                    <Link to="/" className="link_404 inline-block mt-4 px-8 py-2 bg-terracotta text-white">Home</Link>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </div>

    );
};

export default ErrorPage;