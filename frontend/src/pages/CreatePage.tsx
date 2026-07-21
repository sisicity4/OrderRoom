import { useState } from 'react'

function CreatePage() {
  const [title, setTitle] = useState('');
  const [date, setDate] = useState('');
  const [memo, setMemo] = useState('');

  return (
    <div className='flex flex-col min-h-screen bg-[#f0e5cc] gap-4 p-8'>
      <h3 className='tracking-[1.6em] text-neutral-500'>開店</h3>
      <p className='tracking-[0.5em] text-neutral-500'>るーむさくせい</p>
      <h1 className='text-5xl font-bold pb-5'>ルーム作成</h1>

      <div className='flex'>
       <p className='border px-2' >日 付</p>

       <input
       className='border-b-2 border-neutral-700 ml-1.5 outline-none'
       type="date"
       value={date}
       onChange={(e) =>
       setDate(e.target.value)
       } />
      </div>

    <hr className='border-t-5'/ >

      <div className='flex'>
          <p className='border px-2'>ルーム名</p>
          <input
          className='border-b-2 ml-1.5 border-neutral-700 outline-none'
          placeholder='ルーム名を入力'
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
              />
      </div>

      <div className='flex flex-col'>
      <p className='border px-2 w-fit'>メモ</p>
      <textarea
      className='border-y border-dashed mt-2 h-40 outline-none'
      value={memo}
      placeholder='メモ'
      onChange={(e) =>
        setMemo(e.target.value)
      } />
      </div>
      
      <hr className='border-t-5' />
    <div className='flex'>
      <div className='flex flex-col tracking-[0.2em] text-[#7A6B57]'>
        <span>発券後に</span>
        <span>URLが払い出されます。</span>
      </div>
        <button 
        className='ml-auto border-2 w-16 h-16 text-red-700 rounded-full
        [writing-mode:vertical-rl] -rotate-12 font-bold'
        onClick={() => console.log({title,date,memo})}>作成</button>
    </div>
      
      
    </div>
  );
}
export default CreatePage;