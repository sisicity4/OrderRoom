import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

function CreatePage() {
  const [title, setTitle] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [memo, setMemo] = useState('');
  const [budgetAmount,setBudgetAmount]=useState('');
  const [error,setError]=useState('')
  const navigate=useNavigate();
    const handleCreate = async () => {
    if (title.trim() === '') {
      setError('ルーム名を入力してください。');
      return;
    }
    const res = await fetch('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title,
        eventDate: eventDate === '' ? null : eventDate,
        memo,
      }),
    });
    if (!res.ok) {
      setError('ルームの作成に失敗しました。');
      return;
    }
    const data = await res.json();
    navigate(`/rooms/${data.id}`);
  }
  return (
    <div className='flex flex-col min-h-screen bg-[#f0e5cc] gap-4 p-8'>
      <h3 className='tracking-[1.6em] text-[#7A6B57]'>開店</h3>
      <p className='tracking-[0.5em] text-[#7A6B57]'>るーむさくせい</p>
      <h1 className='text-5xl font-bold'>ルーム作成</h1>

      <div className='flex'>
       <p className='border px-2' >日 付</p>

       <input
       className='border-b-2 border-neutral-700 ml-1.5 outline-none'
       type="date"
       value={eventDate}
       onChange={(e) =>
       setEventDate(e.target.value)
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

          <p className='border px-2 ml-70'>予算</p>
          <input
          className='border-b-2 border-neutral-700 outline-none ml-1.5'
          placeholder='予算を入力'
          type="text"
          value={budgetAmount}
          onChange={(e) => setBudgetAmount(e.target.value)}
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
      {error && <p className='text-red-700'>{error}</p>}
    <div className='flex'>
      <div className='flex flex-col tracking-[0.2em] text-[#7A6B57]'>
        <span>発券後に</span>
        <span>URLが払い出されます。</span>
      </div>
      <button onClick={handleCreate}
        className=' ml-auto border-2 w-16 h-16 text-red-700 rounded-full
        [writing-mode:vertical-rl] -rotate-12 font-bold cursor-pointer'>作成
      </button>
    </div>
      
      
    </div>
  );
}
export default CreatePage;