import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';

type Proposal = {
  id: string;
  participantName: string;
  name: string;
  price: number;
  quantity: number;
  memo: string;
  status: string;
  purchased: boolean;
};

type Summary = {
  acceptedTotalPrice: number;
  proposedTotalPrice: number;
  budgetAmount: number | null;
  remainingBudget: number | null;
  overBudgetAmount: number | null;
  byStatus: { proposed: number; accepted: number; rejected: number };
  perParticipant: { participantId: string; name: string; proposalCount: number; acceptedTotalPrice: number }[];
  itemQuantities: { name: string; totalQuantity: number; estimatedTotalPrice: number }[];
};

const statusLabel: Record<string, string> = {
  proposed: '提案中',
  accepted: '採用',
  rejected: '見送り',
};

const stampColor: Record<string, string> = {
  proposed: 'text-[#7A6B57] border-[#7A6B57]',
  accepted: 'text-red-700 border-red-700',
  rejected: 'text-neutral-500 border-neutral-500',
};

const filters = [
  { value: 'all', label: 'すべて' },
  { value: 'proposed', label: '提案中' },
  { value: 'accepted', label: '採用' },
  { value: 'rejected', label: '見送り' },
];

function HostPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const [searchParams] = useSearchParams();
  const hostKey = searchParams.get('key') ?? localStorage.getItem(`host:${roomId}`);
  const [list, setList] = useState<Proposal[]>([]);
  const [error, setError] = useState('');
  const [title, setTitle] = useState('');
  const [filter, setFilter] = useState('all');
  const [summary, setSummary] = useState<Summary | null>(null);

  const shownList = filter === 'all' ? list : list.filter((item) => item.status === filter);

  const fetchRoom = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}`);
      if (!res.ok) {
        setError("ルーム情報の取得に失敗しました");
        return;
      }
      const data = await res.json();
      setTitle(data.title);
    } catch {
      setError("通信に失敗しました");
    }
  }

  const fetchItems = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items`);
      if (!res.ok) {
        setError("提案の取得に失敗しました");
        return;
      }
      const data = await res.json();
      setList(data);
    } catch {
      setError("通信に失敗しました");
    }
  }

  const fetchSummary = async () => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/summary`, {
        headers: {
          'X-Host-Key': hostKey ?? '',
        },
      });
      if (!res.ok) {
        setError("集計の取得に失敗しました");
        return;
      }
      const data = await res.json();
      setSummary(data);
    } catch {
      setError("通信に失敗しました");
    }
  }

  const updateStatus = async (itemId: string, status: string) => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items/${itemId}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Host-Key': hostKey ?? '',
        },
        body: JSON.stringify({ status }),
      });
      if (!res.ok) {
        setError("状態の変更に失敗しました");
        return;
      }
      fetchItems();
      fetchSummary();
    } catch {
      setError("通信に失敗しました");
    }
  }

  const updatePurchased = async (itemId: string, purchased: boolean) => {
    try {
      const res = await fetch(`/api/rooms/${roomId}/items/${itemId}/purchased`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Host-Key': hostKey ?? '',
        },
        body: JSON.stringify({ purchased }),
      });
      if (!res.ok) {
        setError("購入状態の変更に失敗しました");
        return;
      }
      fetchItems();
    } catch {
      setError("通信に失敗しました");
    }
  }

  useEffect(() => {
    fetchRoom();
    fetchItems();
    fetchSummary();
  }, [roomId]);

  if (!hostKey) {
    return (
      <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
        <p>管理用URLが正しくありません。</p>
      </div>
    );
  }

  return (
    <div className='bg-[#f0e5cc] min-h-screen flex flex-col gap-4 p-8'>
      <h3 className='tracking-[1.6em] text-[#7A6B57]'>帳場</h3>
      <p className='tracking-[0.5em] text-[#7A6B57]'>ほすとかんり</p>
      <h1 className='text-[clamp(1.75rem,7vw,2.5rem)] font-bold'>ホスト管理</h1>
      <div className='flex'>
        <p className='border px-2 shrink-0'>ルーム名</p>
        <p className='border-b-2 border-neutral-700 ml-1.5 w-full min-w-0 break-all'>{title}</p>
      </div>

      <hr className='border-t-5' />

      {summary && (
        <div className='flex flex-col gap-4'>
          <p className='border px-2 w-fit'>勘定</p>

          <div className='flex flex-col'>
            <div className='flex border-b border-dashed border-neutral-700 py-1'>
              <p className='shrink-0'>採用の合計</p>
              <p className='ml-auto tracking-[0.2em] font-bold'>{summary.acceptedTotalPrice}円</p>
            </div>
            <div className='flex border-b border-dashed border-neutral-700 py-1 text-[#7A6B57]'>
              <p className='shrink-0'>提案中の合計</p>
              <p className='ml-auto tracking-[0.2em]'>{summary.proposedTotalPrice}円</p>
            </div>
            <div className='flex border-b border-dashed border-neutral-700 py-1'>
              <p className='shrink-0'>予算</p>
              <p className='ml-auto tracking-[0.2em]'>
                {summary.budgetAmount === null ? '未設定' : `${summary.budgetAmount}円`}
              </p>
            </div>
            {summary.budgetAmount !== null && (
              summary.overBudgetAmount !== null && summary.overBudgetAmount > 0 ? (
                <div className='flex border-b border-dashed border-neutral-700 py-1 text-red-700 font-bold'>
                  <p className='shrink-0'>予算超過</p>
                  <p className='ml-auto tracking-[0.2em]'>{summary.overBudgetAmount}円</p>
                </div>
              ) : (
                <div className='flex border-b border-dashed border-neutral-700 py-1'>
                  <p className='shrink-0'>残り</p>
                  <p className='ml-auto tracking-[0.2em]'>{summary.remainingBudget}円</p>
                </div>
              )
            )}
            <p className='text-sm text-[#7A6B57] mt-1'>
              提案中 {summary.byStatus.proposed}件 / 採用 {summary.byStatus.accepted}件 / 見送り {summary.byStatus.rejected}件
            </p>
          </div>

          <div className='flex flex-col'>
            <p className='border px-2 w-fit mb-1'>人別</p>
            {summary.perParticipant.length === 0 && (
              <p className='text-[#7A6B57]'>まだ提案がありません。</p>
            )}
            {summary.perParticipant.map((p) => (
              <div className='flex gap-2 border-b border-dashed border-neutral-700 py-1' key={p.participantId}>
                <p className='min-w-0 break-all'>{p.name}</p>
                <p className='text-sm text-[#7A6B57] shrink-0 self-center'>{p.proposalCount}件</p>
                <p className='ml-auto shrink-0 tracking-[0.2em]'>{p.acceptedTotalPrice}円</p>
              </div>
            ))}
            <p className='text-sm text-[#7A6B57] mt-1'>金額は採用分のみ</p>
          </div>

          <div className='flex flex-col'>
            <p className='border px-2 w-fit mb-1'>品目別</p>
            {summary.itemQuantities.length === 0 && (
              <p className='text-[#7A6B57]'>
                {summary.byStatus.rejected > 0 ? '見送り以外の提案がありません。' : 'まだ提案がありません。'}
              </p>
            )}
            {summary.itemQuantities.map((q) => (
              <div className='flex gap-2 border-b border-dashed border-neutral-700 py-1' key={q.name}>
                <p className='min-w-0 break-all'>{q.name}</p>
                <p className='text-sm text-[#7A6B57] shrink-0 self-center'>{q.totalQuantity}個</p>
                <p className='ml-auto shrink-0 tracking-[0.2em]'>{q.estimatedTotalPrice}円</p>
              </div>
            ))}
            <p className='text-sm text-[#7A6B57] mt-1'>見送りを除いた提案中・採用の合計</p>
          </div>

          <hr className='border-t-5' />
        </div>
      )}

      <div className='flex flex-wrap gap-2'>
        {filters.map((f) => (
          <button
            key={f.value}
            className={`border px-3 py-1 cursor-pointer ${filter === f.value ? 'border-red-700 text-red-700 font-bold' : 'border-dashed'}`}
            onClick={() => setFilter(f.value)}>{f.label}
          </button>
        ))}
      </div>

      {error && <p className='text-red-700'>{error}</p>}

      {shownList.length === 0 && (
        <p className='text-[#7A6B57]'>該当する提案はありません。</p>
      )}
      <ul className='flex flex-col gap-2'>
        {shownList.map((item) => (
          <li
            className={`border border-dashed p-3 flex flex-col gap-3 ${item.status === 'rejected' ? 'opacity-60' : ''}`}
            key={item.id}>
            <div className='flex gap-2'>
              <div className='flex flex-col min-w-0'>
                <p className='font-bold break-all'>{item.name}</p>
                <p>{item.price}円 × {item.quantity}個</p>
                <p className='text-sm text-[#7A6B57] break-all'>提案者: {item.participantName}</p>
                {item.memo && (
                  <p className='text-sm text-[#7A6B57] break-all'>メモ: {item.memo}</p>
                )}
                {item.purchased && (
                  <p className='border border-red-700 text-red-700 px-2 w-fit text-sm mt-1'>購入済</p>
                )}
              </div>
              <p className={`[writing-mode:vertical-rl] border-2 rounded-full w-14 h-14 shrink-0 grid place-items-center -rotate-12 ml-auto font-bold text-sm ${stampColor[item.status] ?? ''}`}>
                {statusLabel[item.status] ?? item.status}
              </p>
            </div>

            <div className='flex flex-wrap gap-2'>
              <button
                className='border px-3 py-1 cursor-pointer disabled:opacity-40 disabled:cursor-default'
                disabled={item.status === 'accepted'}
                onClick={() => updateStatus(item.id, 'accepted')}>採用</button>
              <button
                className='border px-3 py-1 cursor-pointer disabled:opacity-40 disabled:cursor-default'
                disabled={item.status === 'rejected'}
                onClick={() => updateStatus(item.id, 'rejected')}>見送り</button>
              <button
                className='border px-3 py-1 cursor-pointer disabled:opacity-40 disabled:cursor-default'
                disabled={item.status === 'proposed'}
                onClick={() => updateStatus(item.id, 'proposed')}>提案中に戻す</button>
              {item.status === 'accepted' && (
                <button
                  className='border-2 border-red-700 text-red-700 px-3 py-1 cursor-pointer'
                  onClick={() => updatePurchased(item.id, !item.purchased)}>
                  {item.purchased ? '未購入に戻す' : '購入済みにする'}
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
export default HostPage;
