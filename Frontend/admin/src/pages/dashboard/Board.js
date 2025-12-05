import React from 'react';
import { Link } from 'react-router-dom';

const Board = () => {

  return (
    <div className='flex items-center justify-center gap-20 w-full p-[60px]'>

      <section className="grid xl:grid-cols-3 grid-cols-1 w-4/5 gap-5 text-white font-agrandir">

        <Link to={'/dashboard/blogapp'} className='flex flex-col justify-center items-center p-[30px] gap-5 bg-black border-2 rounded-lg shadow-md' id='articles_count'>
          <p className='text-[32px] font-bold'>...</p>
          <p className='text-[18px]'>STATS</p>
        </Link>

        <Link to={'/dashboard/projectapp'} className='flex flex-col justify-center items-center p-[30px] gap-5 bg-black border-2 rounded-lg shadow-md' id='collections_count'>

          <p className='text-[32px] font-bold'>...</p>
          <p className='text-[18px]'>STATS</p>
        </Link>


        <Link to={'/dashboard/team'} className='flex flex-col justify-center items-center p-[30px] gap-5 bg-black border-2 rounded-lg shadow-md' id='rubriques_count'>

          <p className='text-[32px] font-bold'>...</p>
          <p className='text-[18px]'>STATS</p>
        </Link>

      </section>

    </div>
  )
}

export default Board