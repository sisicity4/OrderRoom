import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

function CreatePage() {
  const [title, setTitle] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [memo, setMemo] = useState('');
  const [budgetAmount, setBudgetAmount] = useState('');
  const [error, setError] = useState('')
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);
  const [id, setId] = useState('');
  const [participantUrl, setParticipantUrl] = useState('');
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
    localStorage.setItem(`host:${data.id}`, data.hostKey);
    setId(data.id);
    setParticipantUrl(data.participantUrl);
  }
  return (
    <div className='flex flex-col min-h-screen bg-[#f0e5cc] gap-4 p-8'>
      <h3 className='tracking-[1.6em] text-[#7A6B57]'>開店</h3>
      <p className='tracking-[0.5em] text-[#7A6B57]'>るーむさくせい</p>
      <h1 className='text-[clamp(2rem,8vw,3rem)] font-bold'>ルーム作成</h1>
      <div className='flex'>
        <p className='border px-2 shrink-0' >日 付</p>

        <input
          className='border-b-2 border-neutral-700 ml-1.5 outline-none min-w-0'
          type="date"
          value={eventDate}
          onChange={(e) =>
            setEventDate(e.target.value)
          } />
      </div>

      <hr className='border-t-5' />

      <div className='flex'>
        <p className='border px-2 shrink-0 '>ルーム名</p>
        <input
          className='border-b-2 ml-1.5 border-neutral-700 outline-none w-full min-w-0'
          placeholder='ルーム名を入力'
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <div className='flex'>
        <p className='border px-2 shrink-0'>予算</p>
        <input
          className='border-b-2 border-neutral-700 outline-none ml-1.5 w-full min-w-0'
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
      {id && (
        <div className='flex flex-col gap-2 border border-dashed p-4'>
          <p className='border w-fit px-2'>伝票番号</p>
          <p className='break-all text-sm'>{participantUrl}</p>
          <div className='flex gap-2'>
            <button
              className='border px-3 py-1 cursor-pointer'
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(participantUrl);
                  setCopied(true);
                } catch {
                  setError('コピーできませんでした。URLを長押しして選択してください。');
                }
              }}>URLをコピー</button>
            <button
              className='border px-3 py-1 ml-auto cursor-pointer'
              onClick={() => navigate(`/rooms/${id}`)}>ルームへ進む</button>
          </div>
          {copied && <p className='text-[#7A6B57]'>コピーしました。</p>}
        </div>
      )}
      <div className='flex'>
        <div className='flex flex-col tracking-[0.2em] text-[#7A6B57]'>
          <span>発券後に</span>
          <span>URLが払い出されます。</span>
        </div>
        <button onClick={handleCreate}
          disabled={id !== ''}
          className=' ml-auto shrink-0 border-2 w-16 h-16 text-red-700 rounded-full
        [writing-mode:vertical-rl] -rotate-12 font-bold cursor-pointer'>作成
        </button>
      </div>


    </div>
  );
}
export default CreatePage;