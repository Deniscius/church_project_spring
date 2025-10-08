import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';


const Signin = () => {

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [loginError, setLoginError] = useState(false);


    return (
        <div className='flex flex-col justify-center items-center gap-[30px] p-5 py-10 xl:border-2 rounded-xl xl:w-[400px]'>

            <div className="flex flex-col text-white gap-2.5 justify-start items-start w-full">
                LOGO

                <p className="font-pressura text-[18px]">Se Connecter</p>

                <p className="font-pressura text-[14px]">Connectez-vous en saisissant vos identifiants !</p>

            </div>

            <form className="flex flex-col items-center gap-5 w-full">
                <input
                    className='outline-none border-2 px-4 py-2 rounded-md w-full font-agrandir'
                    type="email"
                    placeholder='Email'
                    value={email} onChange={(e) => setEmail(e.target.value)}
                    required
                />

                <input
                    className='outline-none border-2 px-4 py-2 rounded-md w-full font-agrandir'
                    type="password"
                    placeholder='Mot de passe'
                    value={password} onChange={(e) => setPassword(e.target.value)}
                    required
                />

                <span className={`text-red-400 text-[14px] font-agrandir ${loginError ? 'bloc' : 'hidden'}`}>
                    Email / Mot de passe invalide !
                </span>

                <button className="flex items-center justify-center bg-greenblock rounded-[5px] text-white text-[20px] p-2 font-bold buttonviolet w-[250px!important] hover:w-[260px!important]">

                    {isLoading ? (
                        <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                    ) : (
                        <p>Connexion</p>
                    )}
                </button>

                <div className="w-full text-[14px] flex justify-center gap-1">
                    <Link to={'/passwordrecovery'} className="text-terracotta cursor-pointer">Réinitialiser votre mot de passe !</Link>
                </div>
            </form>

        </div>
    )
}

export default Signin